package cn.yase.share4.demo2;

import cn.yase.share4.demo2.reslover.EtcdNameResolverFactory;
import cn.yase.share4.proto.GreeterGrpc;
import cn.yase.share4.proto.HelloReply;
import cn.yase.share4.proto.HelloRequest;
import io.grpc.LoadBalancerRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * @author yase
 * @create 2019-05-24
 */
public class GrpcClient {

    public static void main(String[] args) throws InterruptedException {
        String target = "etcd:///grpc/java";
        ManagedChannel managedChannel = ManagedChannelBuilder.forTarget(target)
                .nameResolverFactory(new EtcdNameResolverFactory())
                .loadBalancerFactory(LoadBalancerRegistry.getDefaultRegistry().getProvider("round_robin"))
                .usePlaintext()
                .build();

        while (true){
            GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(managedChannel);

            HelloReply helloReply = stub.sayHello(HelloRequest.newBuilder().setName("yase").build());

            System.out.println(helloReply.getMessage());

            Thread.sleep(2000);
        }
    }

}
