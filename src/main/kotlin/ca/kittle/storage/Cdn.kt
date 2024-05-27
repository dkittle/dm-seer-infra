package ca.kittle.storage

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.acm.kotlin.Certificate
import com.pulumi.aws.cloudfront.kotlin.Distribution
import com.pulumi.aws.cloudfront.kotlin.distribution
import com.pulumi.aws.s3.kotlin.Bucket


suspend fun staticWebsiteCdn(env: Stack, bucket: Bucket, cert: Certificate): Distribution {
    val certificateArn = cert.arn.applyValue(fun(arn: String): String { return arn })
    val bucketArn = bucket.arn.applyValue(fun(arn: String): String { return arn })
    val bucketWebsite = bucket.websiteEndpoint.applyValue(fun(website: String): String { return website })
    return distribution("b4b-${env.name}-website-cdn") {
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
            aliases("b4bdev.com", "www.b4bdev.com")
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
                acmCertificateArn("arn:aws:acm:us-east-1:814245790557:certificate/af13b62d-ce6b-4da6-9b21-b158bddfd535")
                sslSupportMethod("sni-only")
            }
            tags(envTags(env, "static-website-cdn"))
        }
    }
}
