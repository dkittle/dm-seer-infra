package ca.kittle

import ca.kittle.cluster.containerRegistry
import ca.kittle.cluster.cluster
import ca.kittle.core.b4bdevCertificate
import ca.kittle.network.domainRecord
import ca.kittle.network.environmentVpc
import ca.kittle.network.privateSubnet
import ca.kittle.security.SecurityGroupContext
import ca.kittle.security.inboundSecurityGroup
import ca.kittle.storage.mongoCluster
import ca.kittle.storage.secureStaticWebsite
import ca.kittle.storage.staticWebsite
import ca.kittle.storage.staticWebsiteCdn
import com.pulumi.Context
import com.pulumi.kotlin.Pulumi
import com.pulumi.kotlin.export
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.runBlocking

val mainScope = MainScope()
const val DOMAIN_NAME_KEY = "fqdn"

fun main() {
    Pulumi.run(::run)
//    Pulumi.run { ctx ->
//    }
}

enum class Stack {
    Dev,
    Staging,
    Prod;

    fun stackEnv(): String = name.lowercase()
}
fun envTags(env: Stack, resource: String): Map<String, String> = mapOf(
    "Name" to "${env.stackEnv()}-$resource",
    "env" to env.name
)

fun run(ctx: Context) {
    runBlocking {
        val env = Stack.valueOf(ctx.stackName().replaceFirstChar { it.uppercase() })
        val domain = ctx.config().require(DOMAIN_NAME_KEY)
        val fqdn = "https://$domain"
        val staticSitePath = "../dm-seer/build/js/packages/dmseer"

        val vpc = environmentVpc(env)
        val privateSubnet = privateSubnet(env, vpc)
        val b4bdevCert = b4bdevCertificate(env)
        val staticWebsite = staticWebsite(env)
        val websitePolicy = secureStaticWebsite(env, staticWebsite)
        val cdn = staticWebsiteCdn(env, staticWebsite, b4bdevCert)
        val devDomain = domainRecord(env, cdn)

        val containerRegistry: String = containerRegistry(env)
        val cluster = cluster(env)

        val mongoSG = inboundSecurityGroup(
            SecurityGroupContext(
                env,
                "mongo",
                "tcp",
                27017,
                vpc
            )
        )

        val mongo = mongoCluster(env, "mongo", mongoSG)
        ctx.export("service domain name", fqdn)
        ctx.export("container registry url", containerRegistry)
        ctx.export("cluster urn", cluster.urn)
    }

//    fun staticWebsite() {
//        Website()
//    }
}

