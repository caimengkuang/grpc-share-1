package cn.yase.share4.demo2.reslover;

import com.google.common.base.Preconditions;
import io.grpc.Attributes;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

import javax.annotation.Nullable;
import java.net.URI;

/**
 * @author yase
 * @create 2019-05-24
 */
public class EtcdNameResolverFactory extends NameResolverProvider {

    private final static String SCHEME = "etcd";

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 5;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI uri, Attributes attributes) {
        if (SCHEME.equals(uri.getScheme())){
            String path = uri.getPath();

            Preconditions.checkNotNull(path,"path is null!");

            return new EtcdNameResolver(path);
        }

        return null;
    }

    @Override
    public String getDefaultScheme() {
        return SCHEME;
    }
}
