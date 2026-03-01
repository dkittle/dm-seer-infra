package ca.kittle

data class ParameterStoreEnvVar(
    val name: String,
    val parameterName: String,
    val withDecryption: Boolean = false,
)

fun apiParameterStoreEnvVars(env: Stack): List<ParameterStoreEnvVar> = listOf(
    ParameterStoreEnvVar("POSTGRES_HOST", "/shared-config/${env.stackName}/postgres/host"),
    ParameterStoreEnvVar("POSTGRES_USERNAME", "/shared-config/${env.stackName}/postgres/dmseer/user/username"),
    ParameterStoreEnvVar("POSTGRES_PASSWORD", "/shared-config/${env.stackName}/postgres/dmseer/user/password", withDecryption = true),
    ParameterStoreEnvVar("ADMIN_USERNAME", "/shared-config/${env.stackName}/dmseer/admin_username"),
    ParameterStoreEnvVar("ADMIN_PASSWORD", "/shared-config/${env.stackName}/dmseer/admin_password", withDecryption = true),
    ParameterStoreEnvVar("SEER_BOT_TOKEN", "/shared-config/${env.stackName}/dmseer/discord_bot_token", withDecryption = true),
    ParameterStoreEnvVar("DMSEER_REDIRECT_URI", "/shared-config/${env.stackName}/dmseer/discord_redirect_uri"),
    ParameterStoreEnvVar("JWT_SECRET", "/shared-config/${env.stackName}/dmseer/jwt_secret", withDecryption = true),
    ParameterStoreEnvVar("POSTMARK_API_KEY", "/shared-config/${env.stackName}/dmseer/postman_apikey", withDecryption = true),
)

fun renderApiTaskDefinitionSecrets(env: Stack): String = apiParameterStoreEnvVars(env).joinToString(",\n") { variable ->
    """
    {
        "name": "${variable.name}",
        "valueFrom": "${variable.parameterArn()}"
    }
    """.trimIndent()
}

fun renderApiEnvFileScript(env: Stack): String = buildString {
    appendLine("#!/bin/bash")
    appendLine("set -euo pipefail")
    appendLine()
    appendLine("tmp_env_file=\"\$(mktemp)\"")
    appendLine("trap 'rm -f \"\$tmp_env_file\"' EXIT")
    appendLine()
    appendLine("append_param() {")
    appendLine("    local env_name=\"\$1\"")
    appendLine("    local parameter_name=\"\$2\"")
    appendLine("    local with_decryption=\"\$3\"")
    appendLine("    local value")
    appendLine()
    appendLine("    if [[ \"\$with_decryption\" == \"true\" ]]; then")
    appendLine("        value=\"\$(aws ssm get-parameter --region $AWS_REGION --name \"\$parameter_name\" --with-decryption --query 'Parameter.Value' --output text)\"")
    appendLine("    else")
    appendLine("        value=\"\$(aws ssm get-parameter --region $AWS_REGION --name \"\$parameter_name\" --query 'Parameter.Value' --output text)\"")
    appendLine("    fi")
    appendLine()
    appendLine("    printf '%s=%s\\n' \"\$env_name\" \"\$value\" >> \"\$tmp_env_file\"")
    appendLine("}")
    appendLine()
    apiParameterStoreEnvVars(env).forEach { variable ->
        appendLine("append_param \"${variable.name}\" \"${variable.parameterName}\" \"${variable.withDecryption}\"")
    }
    appendLine()
    appendLine("install -m 600 \"\$tmp_env_file\" /etc/dmseer-api.env")
}

private fun ParameterStoreEnvVar.parameterArn(): String =
    "arn:aws:ssm:$AWS_REGION:$AWS_ACCOUNT_ID:parameter${parameterName.removePrefix("/")}"
