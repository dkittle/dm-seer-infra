package ca.kittle.network

import ca.kittle.Stack
import com.pulumi.aws.cloudfront.kotlin.Distribution
import com.pulumi.aws.alb.kotlin.LoadBalancer
import com.pulumi.aws.route53.kotlin.Record
import com.pulumi.aws.route53.kotlin.record


private fun domainNames(env: Stack): Pair<String, String> =
    when (env) {
        Stack.Dev -> Pair("dev.quillndice.com", "Z02219281O973CNEZDMPD")
        Stack.Staging -> Pair("stage.quillndice.com", "Z02219281O973CNEZDMPD")
        Stack.Prod -> Pair("quillndice.com", "Z02219281O973CNEZDMPD")
    }

private fun apiDomainNames(env: Stack): Pair<String, String> =
    when (env) {
        Stack.Dev -> Pair("dev.dmseer.quillndice.com", "Z02219281O973CNEZDMPD")
        Stack.Staging -> Pair("stage.dmseer.quillndice.com", "Z02219281O973CNEZDMPD")
        Stack.Prod -> Pair("dmseer.quillndice.com", "Z02219281O973CNEZDMPD")
    }

suspend fun domainRecord(env: Stack, cdn: Distribution): Record {
    val cdnAlias = cdn.domainName.applyValue(fun(name: String): String { return name })
    val cdnZoneId = cdn.hostedZoneId.applyValue(fun(name: String): String { return name })
    return record("${env.name.lowercase()}-qnd-domain-record") {
        args {
            zoneId(domainNames(env).second)
            name(domainNames(env).first)
            aliases {
                name(cdnAlias)
                zoneId(cdnZoneId)
                evaluateTargetHealth(false)
            }
            type("A")
        }
    }
}

suspend fun createApiDomainRecord(env: Stack, loadBalancer: LoadBalancer): Record {
    val alias = loadBalancer.dnsName.applyValue(fun(name: String): String { return name })
    val zoneId = loadBalancer.zoneId.applyValue(fun(name: String): String { return name })
    return record("${if (env != Stack.Prod) "${env.stackName}-" else ""}api") {
        args {
            zoneId(domainNames(env).second)
            name("${if (env != Stack.Prod) "${env.stackName}-" else ""}api.quillndice.com")
            aliases {
                name(alias)
                zoneId(zoneId)
                evaluateTargetHealth(false)
            }
            type("A")
        }
    }
}
