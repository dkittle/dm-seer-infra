package ca.kittle

import com.pulumi.aws.appsync.kotlin.type
import com.pulumi.aws.memorydb.kotlin.acl
import com.pulumi.aws.quicksight.kotlin.folder
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
//        type("aws:s3:BucketPublicAccessBlock")
    }
    val publicBucketAcl = bucketAclV2("b4b-${env.name}-website-public-acl") {
        args {
            bucket(source.id)
            acl("public-read")
        }
        opts {
            dependsOn(ownerControls, publicAccessBlock)
        }
    }

//    val bucketPolicy = bucketPolicy("b4b-${env.name}-website-policy") {
//        args {
//            bucket(source.id)
//            policy("{\"Version\": \"2012-10-17\", \"Statement\": [{ \"Sid\": \"PublicReadGetObject\", \"Effect\": \"Allow\", \"Principal\": \"*\", \"Action\": \"s3:GetObject\", \"Resource\": \"arn:aws:s3:::b4b-dev-website-7c6ace2/*\" }]}") // ktlint-disable max-line-length
//        }
//    }
}
