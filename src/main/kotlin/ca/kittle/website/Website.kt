package ca.kittle.website

import ca.kittle.Stack
import ca.kittle.network.createWebsiteDomainRecord
import ca.kittle.storage.buildCdnForWebsite
import ca.kittle.storage.createWebsiteBucket
import ca.kittle.storage.secureWebsite
import com.pulumi.aws.acm.kotlin.Certificate
import com.pulumi.aws.cloudfront.kotlin.Distribution

suspend fun createWebsite(
    env: Stack,
    name: String,
    domainName: String
): Distribution {
    val bucket = createWebsiteBucket(env, name)
    secureWebsite(env, bucket, name)
    val cdn = buildCdnForWebsite(env, bucket, name, domainName)
    createWebsiteDomainRecord(env, cdn, name, domainName)
    return cdn
}
