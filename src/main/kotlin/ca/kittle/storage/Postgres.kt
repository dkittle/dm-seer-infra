package ca.kittle.storage

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.ec2.kotlin.SecurityGroup
import com.pulumi.aws.rds.kotlin.Instance
import com.pulumi.aws.rds.kotlin.enums.InstanceType
import com.pulumi.aws.rds.kotlin.instance
import com.pulumi.aws.rds.kotlin.parameterGroup
import com.pulumi.aws.ssm.kotlin.SsmFunctions.getParameter
import com.pulumi.core.Output

suspend fun createPostgresDatabase(
    env: Stack,
    securityGroup: SecurityGroup,
    subnetGroupName: Output<String>,
): Instance {
    val securityGroupId = securityGroup.id.applyValue(fun(name: String): String { return name })
    val postgresUsername = getParameter("/shared-config/${env.stackName}/postgres/dmseer/user/username", false)
    val postgresPassword = getParameter("/shared-config/${env.stackName}/postgres/dmseer/user/password", true)
    return instance("${env.stackName}-dmseer-postgres") {
        args {
            identifier("dmseer-postgres-db")
            instanceClass(InstanceType.T4G_Micro)
            allocatedStorage(20)
            engine("postgres")
            engineVersion("16")
            dbName("dmseer")
            username(postgresUsername.value)
            password(postgresPassword.value)
            autoMinorVersionUpgrade(true)
            maxAllocatedStorage(50)
            storageEncrypted(true)
            dbSubnetGroupName(subnetGroupName)
            vpcSecurityGroupIds(listOf(securityGroupId))
            skipFinalSnapshot(true)
            tags(envTags(env, "${env.stackName}-postgres"))
        }
    }
}

