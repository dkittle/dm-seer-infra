package ca.kittle.storage

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.s3.kotlin.*
import com.pulumi.aws.s3.kotlin.bucket


suspend fun staticWebsite(env: Stack): Bucket =
    bucket("b4b-${env.name}-website") {
        args {
            website {
                indexDocument("index.html")
                errorDocument("error.html")
            }
            tags(envTags(env, "static-website-bucket"))
        }
    }

suspend fun secureStaticWebsite(env: Stack, source: Bucket) {
    val ownerControls = bucketOwnershipControls("b4b-${env.name}-website-ownership-controls") {
        args {
            bucket(source.id)
            rule { objectOwnership("BucketOwnerPreferred") }
        }
    }
    val publicAccessBlock = bucketPublicAccessBlock("b4b-${env.name}-website-public-access-block") {
        args {
            bucket(source.id)
            blockPublicAcls(false)
            blockPublicPolicy(false)
            ignorePublicAcls(false)
            restrictPublicBuckets(false)
        }
    }

    val bucketPolicyJson = source.arn.applyValue(fun(arn: String): String {
        return "{\"Version\": \"2012-10-17\", \"Statement\": [{ \"Sid\": \"PublicReadGetObject\", \"Effect\": \"Allow\", \"Principal\": \"*\", \"Action\": \"s3:GetObject\", \"Resource\": \"${arn}/*\" }]}";
    })

    val bucketPolicy = bucketPolicy("b4b-${env.name}-website-policy") {
        args {
            bucket(source.id)
            policy(bucketPolicyJson)
        }
    }

}
