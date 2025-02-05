package ca.kittle

import ca.kittle.cluster.createCluster
import ca.kittle.cluster.createFGTaskDefinition
import ca.kittle.cluster.createService
import ca.kittle.core.qndCertificate
import ca.kittle.network.*
import ca.kittle.security.*
import ca.kittle.storage.createPostgresDatabase
import com.pulumi.Context
import com.pulumi.core.Output
import com.pulumi.kotlin.Pulumi
import com.pulumi.resources.StackReference
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.runBlocking

val mainScope = MainScope()
const val DOMAIN_NAME_KEY = "fqdn"

fun main() {
    Pulumi.run(::run)
}

enum class Stack {
    Dev,
    Staging,
    Prod;

    val stackName: String = name.lowercase()
}

fun envTags(env: Stack, resource: String): Map<String, String> = mapOf(
    "Name" to "${env.stackName}-$resource",
    "Env" to env.name
)

fun run(ctx: Context) {
    runBlocking {
        val env = Stack.valueOf(ctx.stackName().replaceFirstChar { it.uppercase() })
        val infraStack = StackReference("organization/kittle-infra/${env.stackName}")
        val vpcId = infraStack.output("vpcId") as? Output<String> ?: error { "Cannot read vpcId" }
        val publicSubnet1Id = infraStack.output("publicSubnet1Id") as? Output<String> ?: error { "Cannot read public subnet 1 Id" }
        val publicSubnet2Id = infraStack.output("publicSubnet2Id") as? Output<String> ?: error { "Cannot read public subnet 2 Id" }
        val privateSubnetGroupName = infraStack.output("privateSubnetGroupName") as? Output<String> ?: error { "Cannot read private subnet group Id" }
        val routeTableId = infraStack.output("routeTableId") as? Output<String> ?: error { "Cannot read route table Id" }
        val repositoryUrl = infraStack.output("repositoryUrl") as? Output<String> ?: error { "Cannot read vpcId" }

        // Website Certificate
        val qndCert = qndCertificate(env)
        val psqlSG = createPostgresSecurityGroup(env, vpcId)

        // ELB
        val elbSG = createElbSecurityGroup(env, vpcId)
        val loadBalancer = createLoadbalancer(env, publicSubnet1Id, publicSubnet2Id, elbSG)
        val fgTargetGroup = createFargateTargetGroup(env, vpcId)
        val listener = createListeners(env, loadBalancer, fgTargetGroup, qndCert)

        // Postgres
        val postgres = createPostgresDatabase(env, psqlSG, privateSubnetGroupName)

        // ECS
        createInterfaceEndpoints(env, vpcId, routeTableId)
        val ecsSecurityGroup = createEcsSecurityGroup(env, vpcId, psqlSG)
        val taskExecutionRole = createTaskExecutionRole(env)
        val ecsInstanceRole = createEcsInstanceRole(env)
        val cluster = createCluster(env) // create with Container Insights!
        val taskDefinition = createFGTaskDefinition(env, taskExecutionRole)
        val service = createService(env, cluster, taskDefinition, ecsSecurityGroup, publicSubnet1Id, publicSubnet2Id, loadBalancer, fgTargetGroup)

        val apiDomain = createApiDomainRecord(env, loadBalancer)

        val containerRegistryUrl = repositoryUrl.applyValue(fun(name: String): String { return name })
        val loadBalancerDns = loadBalancer.dnsName.applyValue(fun(name: String): String { return name })
        val postgresEndpoint = postgres.endpoint.applyValue(fun(name: String): String { return name })

        ctx.export("containerRegistryUrl", containerRegistryUrl)
        ctx.export("loadBalanceDns", loadBalancerDns)
        ctx.export("postgresEndpoint", postgresEndpoint)
    }

}

