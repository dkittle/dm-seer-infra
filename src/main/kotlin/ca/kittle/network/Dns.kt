package ca.kittle.network

import ca.kittle.Stack
import com.pulumi.aws.cloudfront.kotlin.Distribution
import com.pulumi.aws.route53.kotlin.Record
import com.pulumi.aws.route53.kotlin.record


private fun domainNames(env: Stack): Pair<String, String> =
    when (env) {
        Stack.Dev -> Pair("www.b4bdev.com", "Z00947551AE5IBY3A9TMV")
        Stack.Staging -> Pair("www.b4bstage.com", "Z00947551AE5IBY3A9TMV")
        Stack.Prod -> Pair("www.buddies4baddies.com", "Z00947551AE5IBY3A9TMV")
    }


suspend fun domainRecord(env: Stack, cdn: Distribution): Record {
    val cdnAlias = cdn.domainName.applyValue(fun(name: String): String { return name })
    val cdnZoneId = cdn.hostedZoneId.applyValue(fun(name: String): String { return name })
    return record("${env.name.lowercase()}_domain_record") {
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
