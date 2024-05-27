package ca.kittle.storage

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.docdb.kotlin.cluster
import com.pulumi.aws.ec2.kotlin.SecurityGroup

suspend fun mongoCluster(env: Stack, name: String, securityGroup: SecurityGroup) = cluster(name) {
    args {
        backupRetentionPeriod(7)
        clusterIdentifier("dmseer-mongo")
        engine("docdb")
        masterUsername("admin")
        masterPassword("Garibaldi!")
        availabilityZones(listOf("ca-central-1"))
        preferredBackupWindow("07:00-04:00")
//        instanceClass("db.r5.large")
        storageEncrypted(true)
        vpcSecurityGroupIds(listOf(securityGroup.id))
        tags(envTags(env, "${name}-docdb"))
    }

}