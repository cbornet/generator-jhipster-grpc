package <%=packageName%>.grpc;

import <%=packageName%>.config.DefaultProfileUtil;

import com.google.protobuf.Empty;
import io.github.jhipster.config.JHipsterProperties;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@GRpcService
public class ProfileInfoService extends ProfileInfoServiceGrpc.ProfileInfoServiceImplBase {
    private final JHipsterProperties jHipsterProperties;
    private final Environment env;

    public ProfileInfoService(JHipsterProperties jHipsterProperties, Environment env) {
        this.jHipsterProperties = jHipsterProperties;
        this.env = env;
    }

    @Override
    public void getActiveProfiles(Empty request, StreamObserver<ProfileInfo> responseObserver) {
        ProfileInfo.Builder builder = ProfileInfo.newBuilder();
        String[] activeProfiles = DefaultProfileUtil.getActiveProfiles(env);
        if (activeProfiles != null) {
            builder.addAllActiveProfiles(Arrays.asList(activeProfiles));
        }
        String ribbonEnv = getRibbonEnv(activeProfiles);
        if (ribbonEnv != null) {
            builder.setRibbonEnv(ribbonEnv);
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private String getRibbonEnv(String[] activeProfiles) {
        String[] displayOnActiveProfiles = jHipsterProperties.getRibbon().getDisplayOnActiveProfiles();
        if (displayOnActiveProfiles == null) {
            return null;
        }
        List<String> ribbonProfiles = new ArrayList<>(Arrays.asList(displayOnActiveProfiles));
        List<String> springBootProfiles = Arrays.asList(activeProfiles);
        ribbonProfiles.retainAll(springBootProfiles);
        if (!ribbonProfiles.isEmpty()) {
            return ribbonProfiles.get(0);
        }
        return null;
    }
}
