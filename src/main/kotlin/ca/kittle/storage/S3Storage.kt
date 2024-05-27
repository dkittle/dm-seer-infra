package ca.kittle.storage

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.s3.kotlin.*
import com.pulumi.aws.s3.kotlin.bucket


suspend fun staticWebsite(env: Stack): Bucket =
    bucket("b4b-${env.name.lowercase()}-website") {
        args {
            website {
                indexDocument("index.html")
                errorDocument("error.html")
                routingRules("""
[{
    "Condition": {
        "KeyPrefixEquals": "/storyline"
    },
    "Redirect": {
        "ReplaceKeyPrefixWith": "index.html"
    }
}]
                """)
            }
            tags(envTags(env, "static-website-bucket"))
        }
    }

suspend fun secureStaticWebsite(env: Stack, source: Bucket) {
    val sourceId = source.id.applyValue(fun(id: String): String { return id })

    val ownerControls = bucketOwnershipControls("b4b-${env.name.lowercase()}-website-ownership-controls") {
        args {
            bucket(sourceId)
            rule { objectOwnership("BucketOwnerPreferred") }
        }
    }
    val publicAccessBlock = bucketPublicAccessBlock("b4b-${env.name.lowercase()}-website-public-access-block") {
        args {
            bucket(sourceId)
            blockPublicAcls(false)
            blockPublicPolicy(false)
            ignorePublicAcls(false)
            restrictPublicBuckets(false)
        }
    }

    val publicAccessControl = bucketAclV2("b4b-${env.name.lowercase()}-website-public-access-block") {
        args {
            bucket(sourceId)
            acl("public-read")
        }
    }

    val bucketPolicyJson = source.arn.applyValue(fun(arn: String): String {
        return "{\"Version\": \"2012-10-17\", \"Statement\": [{ \"Sid\": \"PublicReadGetObject\", \"Effect\": \"Allow\", \"Principal\": \"*\", \"Action\": \"s3:GetObject\", \"Resource\": \"${arn}/*\" }]}";
    })

    val bucketPolicy = bucketPolicy("b4b-${env.name.lowercase()}-website-policy") {
        args {
            bucket(sourceId)
            policy(bucketPolicyJson)
        }
    }

}
