package ca.kittle.storage

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.s3.kotlin.Bucket
import com.pulumi.aws.s3.kotlin.bucket
import com.pulumi.aws.s3.kotlin.bucketAclV2
import com.pulumi.aws.s3.kotlin.bucketOwnershipControls
import com.pulumi.aws.s3.kotlin.bucketPolicy
import com.pulumi.aws.s3.kotlin.bucketPublicAccessBlock

suspend fun createWebsiteBucket(
    env: Stack,
    name: String
): Bucket =
    bucket("${env.stackName}-$name-bucket") {
        args {
            website {
                indexDocument("index.html")
                errorDocument("error.html")
                routingRules(
                    """
                    [{
                        "Condition": {
                            "KeyPrefixEquals": "/storyline"
                        },
                        "Redirect": {
                            "ReplaceKeyPrefixWith": "index.html"
                        }
                    }]
                """
                )
            }
            tags(envTags(env, "$name-bucket"))
        }
    }

suspend fun secureWebsite(
    env: Stack,
    source: Bucket,
    name: String
) {
    val sourceId = source.id.applyValue(fun(id: String): String = id)

    bucketOwnershipControls("${env.stackName}-$name-ownership-controls") {
        args {
            bucket(sourceId)
            rule { objectOwnership("BucketOwnerPreferred") }
        }
    }
    bucketPublicAccessBlock("${env.stackName}-$name-public-access-block") {
        args {
            bucket(sourceId)
            blockPublicAcls(false)
            blockPublicPolicy(false)
            ignorePublicAcls(false)
            restrictPublicBuckets(false)
        }
    }

    bucketAclV2("${env.stackName}-$name-bucket-acl") {
        args {
            bucket(sourceId)
            acl("public-read")
        }
    }

    val bucketPolicyJson =
        source.arn.applyValue(
            fun(arn: String): String =
                "{\"Version\": \"2012-10-17\", \"Statement\": [{ \"Sid\": \"PublicReadGetObject\", \"Effect\": \"Allow\", \"Principal\": \"*\", \"Action\": \"s3:GetObject\", \"Resource\": \"$arn/*\" }]}"
        )

    bucketPolicy("${env.stackName}-$name-policy") {
        args {
            bucket(sourceId)
            policy(bucketPolicyJson)
        }
    }
}
