package ca.kittle;

import com.pulumi.Context
import com.pulumi.Pulumi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

val mainScope = MainScope()

fun main() {
    Pulumi.run(::run)
}


fun run(ctx: Context) {
    mainScope.launch {
        val env = ctx.stackName() ?: "unknown"
        val vpc = vpc(env)
        val publicSubnet = publicSubnet(env, vpc)
    }
}
