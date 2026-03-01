package ca.kittle.storage

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.cloudfront.kotlin.Distribution
import com.pulumi.aws.cloudfront.kotlin.distribution
import com.pulumi.aws.s3.kotlin.Bucket

suspend fun buildCdnForWebsite(
    env: Stack,
    bucket: Bucket,
    name: String,
    domainName: String
): Distribution {
    val bucketArn = bucket.arn.applyValue(fun(arn: String): String = arn)
    val bucketWebsite = bucket.websiteEndpoint.applyValue(fun(website: String): String = website)
    return distribution("${env.stackName}-$name-cdn") {
        args {
            customErrorResponses {
                errorCode(404)
                responseCode(404)
                responsePagePath("/error.html")
            }
            defaultCacheBehavior {
                allowedMethods("GET", "HEAD", "OPTIONS")
                cachedMethods("GET", "HEAD", "OPTIONS")
                compress(true)
                defaultTtl(600)
                maxTtl(600)
                minTtl(600)
                targetOriginId(bucketArn)
                viewerProtocolPolicy("redirect-to-https")
                forwardedValues {
                    cookies {
                        forward("all")
                    }
                    queryString(true)
                }
            }
            enabled(true)
            defaultRootObject("index.html")
            aliases("${env.subdomain()}$domainName")
            origins {
                customOriginConfig {
                    httpPort(80)
                    httpsPort(443)
                    originProtocolPolicy("http-only")
                    originSslProtocols("TLSv1.2")
                }
                domainName(bucketWebsite)
                originId(bucketArn)
            }
            priceClass("PriceClass_100")
            restrictions {
                geoRestriction {
                    restrictionType("none")
                }
            }
            viewerCertificate {
                cloudfrontDefaultCertificate(false)
                acmCertificateArn("arn:aws:acm:us-east-1:814245790557:certificate/c0469e32-9ebb-4fe1-8552-f0c61036756d")
                sslSupportMethod("sni-only")
            }
            tags(envTags(env, "$name-cdn"))
        }
    }
}
