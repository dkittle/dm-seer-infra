package ca.kittle

import ca.kittle.compute.createEc2Instance
import ca.kittle.network.*
import ca.kittle.security.*
import ca.kittle.storage.createPostgresDatabase
import com.pulumi.Config
import com.pulumi.aws.ec2.kotlin.enums.InstanceType
import com.pulumi.aws.lb.kotlin.targetGroupAttachment
import com.pulumi.Context
import com.pulumi.core.Output
import com.pulumi.kotlin.Pulumi
import com.pulumi.resources.StackReference
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.runBlocking

val mainScope = MainScope()
const val DOMAIN_NAME_KEY = "fqdn"
private const val DEFAULT_CERTIFICATE_ARN = "arn:aws:acm:ca-central-1:814245790557:certificate/fa959296-b8a7-4465-b1d5-309ca005ee0a"
private const val AWS_REGION = "ca-central-1"

fun main() {
    Pulumi.run(::run)
}

enum class Stack {
    Dev,
    Staging,
    Prod;

    val stackName: String = name.lowercase()
}

fun envTags(env: Stack, resource: String): Map<String, String> = mapOf(
    "Name" to "${env.stackName}-$resource",
    "Env" to env.name
)

private val dockerUserData = """
    #!/bin/bash
    dnf install -y awscli
    systemctl enable --now docker
""".trimIndent()

private fun websiteUserData(websiteImage: Output<String>, apiPrivateIp: Output<String>): Output<String> =
    websiteImage.apply(fun(image: String): Output<String> {
        return apiPrivateIp.applyValue(fun(ip: String): String {
            val ecrHost = image.substringBefore("/")
            return """
                #!/bin/bash
                dnf update -y
                dnf install -y nginx awscli
                cat <<'EOF' >/usr/local/bin/ecr-login.sh
                #!/bin/bash
                aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ecrHost
                EOF
                chmod +x /usr/local/bin/ecr-login.sh
                cat <<'EOF' >/etc/systemd/system/dmseer-website.service
                [Unit]
                Description=DM Seer website container
                Requires=docker.service
                After=docker.service

                [Service]
                Restart=always
                ExecStartPre=/bin/sh -c '/usr/local/bin/ecr-login.sh'
                ExecStartPre=/usr/bin/docker pull $image
                ExecStartPre=-/usr/bin/docker rm -f dmseer-website
                ExecStart=/usr/bin/docker run --rm --name dmseer-website -p 127.0.0.1:3000:80 $image
                ExecStop=/usr/bin/docker stop dmseer-website

                [Install]
                WantedBy=multi-user.target
                EOF
                cat <<'EOF' >/etc/nginx/conf.d/dmseer.conf
                server {
                    listen 80 default_server;
                    server_name _;

                    location /api/ {
                        proxy_pass http://$ip:8081/;
                        proxy_http_version 1.1;
                        proxy_set_header Host ${'$'}host;
                        proxy_set_header X-Real-IP ${'$'}remote_addr;
                        proxy_set_header X-Forwarded-For ${'$'}proxy_add_x_forwarded_for;
                        proxy_set_header X-Forwarded-Proto ${'$'}scheme;
                    }

                    location / {
                        proxy_pass http://127.0.0.1:3000;
                        proxy_http_version 1.1;
                        proxy_set_header Host ${'$'}host;
                        proxy_set_header X-Real-IP ${'$'}remote_addr;
                        proxy_set_header X-Forwarded-For ${'$'}proxy_add_x_forwarded_for;
                        proxy_set_header X-Forwarded-Proto ${'$'}scheme;
                    }
                }
                EOF
                systemctl daemon-reload
                systemctl enable --now docker
                systemctl enable --now dmseer-website
                systemctl enable --now nginx
            """.trimIndent()
        })
    })

private fun apiUserData(env: Stack, apiImage: Output<String>, postgresEndpoint: Output<String>): Output<String> =
    apiImage.apply(fun(image: String): Output<String> {
        return postgresEndpoint.applyValue(fun(postgresHost: String): String {
            val ecrHost = image.substringBefore("/")
            return """
                #!/bin/bash
                dnf install -y awscli
                cat <<'EOF' >/usr/local/bin/ecr-login.sh
                #!/bin/bash
                aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ecrHost
                EOF
                chmod +x /usr/local/bin/ecr-login.sh
                cat <<'EOF' >/usr/local/bin/render-api-env.sh
                #!/bin/bash
                cat >/etc/dmseer-api.env <<ENVVARS
                POSTGRES_HOST=$postgresHost
                POSTGRES_USERNAME=$(aws ssm get-parameter --region $AWS_REGION --name /shared-config/${env.stackName}/postgres/dmseer/user/username --query Parameter.Value --output text)
                POSTGRES_PASSWORD=$(aws ssm get-parameter --region $AWS_REGION --name /shared-config/${env.stackName}/postgres/dmseer/user/password --with-decryption --query Parameter.Value --output text)
                ADMIN_USERNAME=$(aws ssm get-parameter --region $AWS_REGION --name /shared-config/${env.stackName}/dmseer/admin_username --query Parameter.Value --output text)
                ADMIN_PASSWORD=$(aws ssm get-parameter --region $AWS_REGION --name /shared-config/${env.stackName}/dmseer/admin_password --with-decryption --query Parameter.Value --output text)
                SEER_BOT_TOKEN=$(aws ssm get-parameter --region $AWS_REGION --name /shared-config/${env.stackName}/dmseer/discord_bot_token --with-decryption --query Parameter.Value --output text)
                DMSEER_REDIRECT_URI=$(aws ssm get-parameter --region $AWS_REGION --name /shared-config/${env.stackName}/dmseer/discord_redirect_uri --query Parameter.Value --output text)
                JWT_SECRET=$(aws ssm get-parameter --region $AWS_REGION --name /shared-config/${env.stackName}/dmseer/jwt_secret --with-decryption --query Parameter.Value --output text)
                POSTMARK_API_KEY=$(aws ssm get-parameter --region $AWS_REGION --name /shared-config/${env.stackName}/dmseer/postman_apikey --with-decryption --query Parameter.Value --output text)
                ENVVARS
                EOF
                chmod +x /usr/local/bin/render-api-env.sh
                cat <<'EOF' >/etc/systemd/system/dmseer-api.service
                [Unit]
                Description=DM Seer API container
                Requires=docker.service
                After=docker.service

                [Service]
                Restart=always
                ExecStartPre=/bin/sh -c '/usr/local/bin/render-api-env.sh'
                ExecStartPre=/bin/sh -c '/usr/local/bin/ecr-login.sh'
                ExecStartPre=/usr/bin/docker pull $image
                ExecStartPre=-/usr/bin/docker rm -f dmseer-api
                ExecStart=/usr/bin/docker run --rm --name dmseer-api --env-file /etc/dmseer-api.env -p 8081:8081 $image
                ExecStop=/usr/bin/docker stop dmseer-api

                [Install]
                WantedBy=multi-user.target
                EOF
                systemctl daemon-reload
                systemctl enable --now docker
                systemctl enable --now dmseer-api
            """.trimIndent()
        })
    })

fun run(ctx: Context) {
    runBlocking {
        val env = Stack.valueOf(ctx.stackName().replaceFirstChar { it.uppercase() })
        val config = Config.of("dmseer-infra")
        val certificateArn = config.get("certificateArn").orElse(DEFAULT_CERTIFICATE_ARN)
        val websiteImageTag = config.get("websiteImageTag").orElse("website")
        val apiImageTag = config.get("apiImageTag").orElse("api")
        val infraStack = StackReference("organization/kittle-infra/${env.stackName}")
        val vpcId = infraStack.output("vpcId") as? Output<String> ?: error { "Cannot read vpcId" }
        val publicSubnet1Id = infraStack.output("publicSubnet1Id") as? Output<String> ?: error { "Cannot read public subnet 1 Id" }
        val publicSubnet2Id = infraStack.output("publicSubnet2Id") as? Output<String> ?: error { "Cannot read public subnet 2 Id" }
        val privateSubnet1Id = infraStack.output("privateSubnet1Id") as? Output<String> ?: error { "Cannot read private subnet 1 Id" }
        val privateSubnet2Id = infraStack.output("privateSubnet2Id") as? Output<String> ?: error { "Cannot read private subnet 2 Id" }
        val privateSubnetGroupName = infraStack.output("privateSubnetGroupName") as? Output<String> ?: error { "Cannot read private subnet group Id" }
        val repositoryUrl = infraStack.output("repositoryUrl") as? Output<String> ?: error { "Cannot read vpcId" }

        val elbSG = createElbSecurityGroup(env, vpcId)
        val websiteSecurityGroup = createWebsiteSecurityGroup(env, vpcId, elbSG)
        val apiSecurityGroup = createApiSecurityGroup(env, vpcId, websiteSecurityGroup)
        val endpointSecurityGroup = createPrivateEndpointSecurityGroup(env, vpcId, apiSecurityGroup)
        val psqlSG = createPostgresSecurityGroup(env, vpcId, apiSecurityGroup)

        val privateRouteTable = createPrivateRouteTable(env, vpcId)
        associatePrivateSubnetRouteTable(env, "private-subnet-1-route-association", privateSubnet1Id, privateRouteTable)
        associatePrivateSubnetRouteTable(env, "private-subnet-2-route-association", privateSubnet2Id, privateRouteTable)
        val privateRouteTableId = privateRouteTable.id.applyValue(fun(id: String): String { return id })
        createPrivateInstanceEndpoints(env, vpcId, privateSubnet1Id, privateSubnet2Id, privateRouteTableId, endpointSecurityGroup)

        // Postgres
        val postgres = createPostgresDatabase(env, psqlSG, privateSubnetGroupName)
        val postgresEndpoint = postgres.endpoint.applyValue(fun(name: String): String { return name })
        val websiteImage = repositoryUrl.applyValue(fun(name: String): String { return "$name:$websiteImageTag" })
        val apiImage = repositoryUrl.applyValue(fun(name: String): String { return "$name:$apiImageTag" })

        val loadBalancer = createLoadbalancer(env, publicSubnet1Id, publicSubnet2Id, elbSG)
        val websiteTargetGroup = createWebsiteTargetGroup(env, vpcId)
        createListeners(env, loadBalancer, websiteTargetGroup, certificateArn)
        createWebsiteDomainAliasRecord(env, loadBalancer)

        // EC2
        val ec2ContainerHostProfile = createEc2ContainerHostProfile(env)
        val instanceProfileName = ec2ContainerHostProfile.name.applyValue(fun(name: String): String { return name })
        val apiInstance = createEc2Instance(
            stack = env,
            resourceName = "api-instance",
            subnetId = privateSubnet1Id,
            securityGroups = listOf(apiSecurityGroup),
            instanceType = InstanceType.T4g_Medium,
            iamInstanceProfileName = instanceProfileName,
            userData = apiUserData(env, apiImage, postgresEndpoint),
        )
        val apiPrivateIp = apiInstance.privateIp.applyValue(fun(ip: String): String { return ip })
        val websiteInstance = createEc2Instance(
            stack = env,
            resourceName = "website-instance",
            subnetId = publicSubnet1Id,
            securityGroups = listOf(websiteSecurityGroup),
            instanceType = InstanceType.T4g_Micro,
            associatePublicIpAddress = true,
            iamInstanceProfileName = instanceProfileName,
            userData = websiteUserData(websiteImage, apiPrivateIp),
        )
        val websiteTargetGroupArn = websiteTargetGroup.arn.applyValue(fun(arn: String): String { return arn })
        val websiteInstanceId = websiteInstance.id.applyValue(fun(id: String): String { return id })
        targetGroupAttachment("${env.stackName}-website-target-attachment") {
            args {
                targetGroupArn(websiteTargetGroupArn)
                targetId(websiteInstanceId)
                port(80)
            }
        }

        val containerRegistryUrl = repositoryUrl.applyValue(fun(name: String): String { return name })
        val loadBalancerDns = loadBalancer.dnsName.applyValue(fun(name: String): String { return name })
        val websitePublicIp = websiteInstance.publicIp.applyValue(fun(ip: String): String { return ip })

        ctx.export("containerRegistryUrl", containerRegistryUrl)
        ctx.export("postgresEndpoint", postgresEndpoint)
        ctx.export("loadBalanceDns", loadBalancerDns)
        ctx.export("websitePublicIp", websitePublicIp)
        ctx.export("apiPrivateIp", apiPrivateIp)
    }

}
