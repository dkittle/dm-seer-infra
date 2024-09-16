package ca.kittle

import ca.kittle.cluster.createCluster
import ca.kittle.cluster.createFGTaskDefinition
import ca.kittle.cluster.createService
import ca.kittle.core.qndCertificate
import ca.kittle.network.*
import ca.kittle.security.*
import ca.kittle.storage.containerRegistry
import ca.kittle.storage.mongoCluster
import ca.kittle.storage.mongoInstances
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
/**
        val domain = ctx.config().require(DOMAIN_NAME_KEY)
        val fqdn = "https://$domain"
        val staticSitePath = "../dm-seer/build/js/packages/dmseer"
*/
        // VPC
/**
        val vpc = createVpc(env)
        val publicSubnet = publicSubnet(env, vpc)
        val publicSubnet2 = publicSubnet2(env, vpc)
        val privateSubnet = privateSubnet(env, vpc)
        val privateSubnet2 = privateSubnet2(env, vpc)
        val publicSubnetGroup = publicSubnetGroup(env, publicSubnet, privateSubnet2)
        val privateSubnetGroup = privateSubnetGroup(env, privateSubnet, privateSubnet2)
        val igw = createInternetGateway(env, vpc)
        updateRouteTable(env, vpc, igw)
*/
//        val configStack = StackReference("donkittle/infra-shared-config/${env.stackName}")
        val infraStack = StackReference("donkittle/kittle-infra/${env.stackName}")
        val vpcId = infraStack.output("vpcId") as? Output<String> ?: error { "Cannot read vpcId" }
        val publicSubnet1Id = infraStack.output("publicSubnet1Id") as? Output<String> ?: error { "Cannot read public subnet 1 Id" }
        val publicSubnet2Id = infraStack.output("publicSubnet2Id") as? Output<String> ?: error { "Cannot read public subnet 2 Id" }
        val routeTableId = infraStack.output("routeTableId") as? Output<String> ?: error { "Cannot read route table Id" }

        // Website Certificate
        val qndCert = qndCertificate(env)
        val mongoSG = createMongoSecurityGroup(env, vpcId)
        // ELB
        val elbSG = createElbSecurityGroup(env, vpcId)
        val loadBalancer = createLoadbalancer(env, publicSubnet1Id, publicSubnet2Id, elbSG)
        val fgTargetGroup = createFargateTargetGroup(env, vpcId)
        val listener = createListeners(env, loadBalancer, fgTargetGroup, qndCert)

        // Static website in CDN
/**
        val staticWebsite = staticWebsite(env)
        val websitePolicy = secureStaticWebsite(env, staticWebsite)
        val cdn = staticWebsiteCdn(env, staticWebsite, qndCert)
        val domain = domainRecord(env, cdn)
*/
        // Mongo
/**
        val mongoSG = createMongoSecurityGroup(env, vpc)
        val mongo = mongoCluster(env, mongoSG, privateSubnetGroup, configStack)
        mongoInstances(env, mongo)
*/
        // ECR
/**
 * val containerRegistry = containerRegistry(env)
 */


        // ECS
        createInterfaceEndpoints(env, vpcId, routeTableId)

        val ecsSecurityGroup = createEcsSecurityGroup(env, vpcId, mongoSG)
/**
        val group = createAutoScalingGroup(env, targetGroup, ecsSecurityGroup, publicSubnet, publicSubnet2)
*/
        val taskExecutionRole = createTaskExecutionRole(env)
        val ecsInstanceRole = createEcsInstanceRole(env)
        val cluster = createCluster(env) // create with Container Insights!
        val taskDefinition = createFGTaskDefinition(env, taskExecutionRole)
        val service = createService(env, cluster, taskDefinition, ecsSecurityGroup, publicSubnet1Id, publicSubnet2Id, loadBalancer, fgTargetGroup)
        // associate the alb with the service
        // the container is the name used in the task definition



        val apiDomain = createApiDomainRecord(env, loadBalancer)

//        val k8sRole = createK8SRole()
//        val k8sCluster = cluster(env, k8sRole, privateSubnet, privateSubnet2)

//        val containerRegistryUrl = containerRegistry.repositoryUrl.applyValue(fun(name: String): String { return name })
        val loadBalancerDns = loadBalancer.dnsName.applyValue(fun(name: String): String { return name })
//        val clusterEndpoint = mongo.endpoint.applyValue(fun(name: String): String { return name })
//        val autoScalingGroupName = group.name.applyValue(fun(name: String): String { return name })

//        val k8sEndpoint = k8sCluster.endpoint.applyValue(fun(endpoint: String): String { return endpoint })
//        val cdnUrl = cdn.domainName.applyValue(fun(name: String): String { return name })

//        ctx.export("service domain name", fqdn)
//        ctx.export("cdn url", containerRegistryUrl)
//        ctx.export("mongoClusterEndpoint", clusterEndpoint)
//        ctx.export("containerRegistryUrl", containerRegistryUrl)
        ctx.export("loadBalanceDns", loadBalancerDns)
//        ctx.export("serviceUrl", Output.of("http://$autoScalingGroupName.amazonaws.com"))
//        ctx.export("k8sEndpoint", k8sEndpoint)
    }

//    fun staticWebsite() {
//        Website()
//    }
}

