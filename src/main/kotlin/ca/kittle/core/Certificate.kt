package ca.kittle.core

import ca.kittle.Stack
import ca.kittle.envTags
import com.pulumi.aws.acm.kotlin.certificate


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

suspend fun qndCertificate(env: Stack) =
    certificate("qnd-${env.name.lowercase()}-certificate") {
        args {
            domainName("*.quillndice.com")
            keyAlgorithm("RSA_2048")
//            options {
//                certificateTransparencyLoggingPreference("DISABLED")
//            }
            subjectAlternativeNames("*.quillndice.com", "quillndice.com")
            validationMethod("DNS")
//            validationOptions {
//                domainName("b4bdev.com")
//                validationDomain("b4bdev.com")
//            }
            tags(envTags(env, "qnd-cert"))
        }
        opts {
            retainOnDelete(true)
        }
    }
