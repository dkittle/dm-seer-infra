package ca.kittle

import com.pulumi.aws.acm.kotlin.certificate
import com.pulumi.aws.appsync.kotlin.domainName


/**
 * public static void stack(Context ctx) {
var cert = new Certificate("cert", CertificateArgs.builder()
.domainName("*.b4bdev.com")
.keyAlgorithm("RSA_2048")
.options(CertificateOptionsArgs.builder()
.certificateTransparencyLoggingPreference("DISABLED")
.build())
.subjectAlternativeNames(
"*.b4bdev.com",
"b4bdev.com")
.validationMethod("NONE")
.build(), CustomResourceOptions.builder()
.protect(true)
.build());

}

 */

suspend fun b4bdevCertificate(env: Stack) =
    certificate("b4bdev-${env.name.lowercase()}-certificate") {
        args {
            domainName("*.b4bdev.com")
            keyAlgorithm("RSA_2048")
            options {
                certificateTransparencyLoggingPreference("DISABLED")
            }
            subjectAlternativeNames("*.b4bdev.com", "b4bdev.com")
            validationMethod("DNS")
            validationOptions {
                domainName("b4bdev.com")
                validationDomain("b4bdev.com")
            }
            tags(envTags(env, "b4bdev-cert"))
        }
        opts {
            retainOnDelete(true)
        }
    }
