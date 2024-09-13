package ca.kittle.storage

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.ecr.kotlin.Repository
import com.pulumi.aws.ecr.kotlin.repository

suspend fun containerRegistry(env: Stack): Repository =
    repository("${env.name.lowercase()}-dmseer-repository") {
        args {
            tags(envTags(env, "container-repository"))
        }
    }
