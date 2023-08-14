package ca.kittle

import com.pulumi.Context
import com.pulumi.kotlin.Pulumi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.runBlocking

val mainScope = MainScope()

fun main() {
    Pulumi.run(::run)
//    Pulumi.run { ctx ->
//    }
}

enum class Stack {
    Dev,
    Staging,
    Prod
}

fun envTags(env: Stack, resource: String): Map<String, String> = mapOf(
    "Name" to "${env.name.lowercase()}-$resource",
    "env" to env.name
)

fun run(ctx: Context) {
    runBlocking {
        val env = Stack.valueOf(ctx.stackName().replaceFirstChar { it.uppercase() })
        val vpc = environmentVpc(env)
        val privateSubnet = privateSubnet(env, vpc)
        val staticWebsite = staticWebsite(env)
        val websitePolicy = secureStaticWebsite(env, staticWebsite)
    }
}
