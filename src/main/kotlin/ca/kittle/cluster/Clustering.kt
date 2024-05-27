package ca.kittle.cluster

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.ecr.kotlin.repository
import com.pulumi.aws.ecs.kotlin.cluster


suspend fun cluster(env: Stack) =
    cluster("${env.stackEnv()}-dmseer-cluster") {
        args {
            tags(envTags(env, "cluster"))
        }
    }

suspend fun containerRegistry(env: Stack): String {
    val repo = repository("dmseer-repository") {
        args {
            tags(envTags(env, "container-registry"))
        }
    }
    val repoUrl = repo.repositoryUrl.applyValue(fun(name: String): String { return name })
    return repoUrl.toString()
}
