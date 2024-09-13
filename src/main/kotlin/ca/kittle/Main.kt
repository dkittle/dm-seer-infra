package ca.kittle

import ca.kittle.cluster.createCluster
import ca.kittle.cluster.createService
import ca.kittle.cluster.createTaskDefinition
import ca.kittle.compute.createAutoScalingGroup
import ca.kittle.core.qndCertificate
import ca.kittle.network.*
import ca.kittle.security.*
import ca.kittle.storage.containerRegistry
import com.pulumi.Context
import com.pulumi.kotlin.Pulumi
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

    val stackEnv: String = name.lowercase()
}

fun envTags(env: Stack, resource: String): Map<String, String> = mapOf(
    "Name" to "${env.stackEnv}-$resource",
    "Env" to env.name
)

fun run(ctx: Context) {
    runBlocking {
        val env = Stack.valueOf(ctx.stackName().replaceFirstChar { it.uppercase() })
        val domain = ctx.config().require(DOMAIN_NAME_KEY)
        val fqdn = "https://$domain"
        val staticSitePath = "../dm-seer/build/js/packages/dmseer"

        val vpc = createVpc(env)
        val publicSubnet = publicSubnet(env, vpc)
        val publicSubnet2 = publicSubnet2(env, vpc)
        val privateSubnet = privateSubnet(env, vpc)
        val privateSubnet2 = privateSubnet2(env, vpc)
        val publicSubnetGroup = publicSubnetGroup(env, publicSubnet, privateSubnet2)
        val privateSubnetGroup = privateSubnetGroup(env, privateSubnet, privateSubnet2)
        val igw = createInternetGateway(env, vpc)
        updateRouteTable(env, vpc, igw)
        createInterfaceEndpoints(env, vpc)

        val qndCert = qndCertificate(env)
//        val staticWebsite = staticWebsite(env)
//        val websitePolicy = secureStaticWebsite(env, staticWebsite)
//        val cdn = staticWebsiteCdn(env, staticWebsite, qndCert)
//        val domain = domainRecord(env, cdn)

        val containerRegistry = containerRegistry(env)

//        val mongoSG = inboundSecurityGroup(
//            SecurityGroupContext(
//                env,
//                "mongo",
//                "tcp",
//                27017,
//                vpc
//            )
//        )
//        val mongo = mongoCluster(env, mongoSG, privateSubnetGroup)
//        mongoInstances(env, mongo)

        val ecsSecurityGroup = createEcsSecurityGroup(env, vpc)
        val targetGroup = createTargetGroup(env, vpc)
        val group = createAutoScalingGroup(env, targetGroup, ecsSecurityGroup, publicSubnet, publicSubnet2)
        val loadBalancer = createLoadbalancer(env, publicSubnet, publicSubnet2)
        val listener = createListener(env, loadBalancer, targetGroup, qndCert)

        val taskExecutionRole = createTaskExecutionRole(env)
        val ecsInstanceRole = createEcsInstanceRole(env)
        val cluster = createCluster(env) // create with Container Insights!
        val taskDefinition = createTaskDefinition(env, taskExecutionRole)
        val service = createService(env, cluster, taskDefinition, ecsSecurityGroup, publicSubnet, publicSubnet2)
        // associate the alb with the service
        // the container is the name used in the task definition



//        val apiDomain = createApiDomainRecord(env, group)

//        val k8sRole = createK8SRole()
//        val k8sCluster = cluster(env, k8sRole, privateSubnet, privateSubnet2)

        val containerRegistryUrl = containerRegistry.repositoryUrl.applyValue(fun(name: String): String { return name })
        val loadBalancerDns = loadBalancer.dnsName.applyValue(fun(name: String): String { return name })
//        val clusterUrn = mongo.urn.applyValue(fun(name: String): String { return name })
        val autoScalingGroupName = group.name.applyValue(fun(name: String): String { return name })

//        val k8sEndpoint = k8sCluster.endpoint.applyValue(fun(endpoint: String): String { return endpoint })
//        val cdnUrl = cdn.domainName.applyValue(fun(name: String): String { return name })

//        ctx.export("service domain name", fqdn)
//        ctx.export("cdn url", containerRegistryUrl)
//        ctx.export("mongoClusterUrn", clusterUrn)
        ctx.export("containerRegistryUrl", containerRegistryUrl)
        ctx.export("loadBalanceDns", loadBalancerDns)
//        ctx.export("serviceUrl", Output.of("http://$autoScalingGroupName.amazonaws.com"))
//        ctx.export("k8sEndpoint", k8sEndpoint)
    }

//    fun staticWebsite() {
//        Website()
//    }
}

