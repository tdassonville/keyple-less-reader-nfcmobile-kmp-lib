module keypleless.readerjvm {
    exports org.eclipse.keyple.keyplelessreaderlib;
    requires java.smartcardio;

    requires kotlin.stdlib;
    requires kotlinx.serialization.core;
    requires kotlinx.datetime;
    requires kotlinx.coroutines.core;
    requires napier.jvm;
}