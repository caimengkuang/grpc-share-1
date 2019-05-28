package cn.yase.share4.demo1;

import cn.yase.share4.proto.GreeterGrpc;
import cn.yase.share4.proto.HelloReply;
import cn.yase.share4.proto.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * @author yase
 * @create 2019-05-24
 */
public class GrpcClient {

    public static void main(String[] args) throws InterruptedException {

        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("10.20.0.108", 7001).usePlaintext().build();

        while (true){
            GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(managedChannel);
            HelloReply helloReply = stub.sayHello(HelloRequest.newBuilder().setName("yase").build());
            System.out.println(helloReply.getMessage());

            Thread.sleep(1000);
        }
    }

}
