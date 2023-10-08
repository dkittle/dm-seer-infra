package ca.kittle

import com.pulumi.aws.acm.kotlin.Certificate
import com.pulumi.aws.cloudfront.kotlin.distribution
import com.pulumi.aws.s3.kotlin.Bucket

suspend fun staticWebsiteCdn(env: Stack, bucket: Bucket, cert: Certificate) =
    distribution("b4b-${env.name}-website-cdn") {
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
                targetOriginId(bucket.arn)
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
            aliases("${env.name.lowercase()}.b4bdev.com")
            origins {
                customOriginConfig {
                    httpPort(80)
                    httpsPort(443)
                    originProtocolPolicy("http-only")
                    originSslProtocols("TLSv1.2")
                }
                domainName(bucket.websiteEndpoint)
                originId(bucket.arn)
            }
            priceClass("PriceClass_100")
            restrictions {
                geoRestriction {
                    restrictionType("none")
                }
            }
            viewerCertificate {
                cloudfrontDefaultCertificate(false)
                acmCertificateArn(cert.arn)
                sslSupportMethod("sni-only")
            }
            tags(envTags(env, "static-website-cdn"))
        }
    }
