package ca.kittle.storage

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.docdb.kotlin.Cluster
import com.pulumi.aws.docdb.kotlin.SubnetGroup
import com.pulumi.aws.docdb.kotlin.cluster
import com.pulumi.aws.ec2.kotlin.SecurityGroup

suspend fun mongoCluster(
    env: Stack, name: String,
    securityGroup: SecurityGroup,
    subnetGroup: SubnetGroup
): Cluster {
    val securityGroupId = securityGroup.id.applyValue(fun(name: String): String { return name })
    val subnetGroupName = subnetGroup.name.applyValue(fun(name: String): String { return name })
    return cluster(name) {
        args {
            backupRetentionPeriod(7)
            clusterIdentifier("dmseer-mongo")
            dbSubnetGroupName(subnetGroupName)
            engine("docdb")
            masterUsername("admin")
            masterPassword("Garibaldi!")
            availabilityZones(listOf("ca-central-1"))
            preferredBackupWindow("07:00-04:00")
//        instanceClass("db.r5.large")
            storageEncrypted(true)
            vpcSecurityGroupIds(listOf(securityGroupId))
            tags(envTags(env, "${name}-docdb"))
        }

    }
}