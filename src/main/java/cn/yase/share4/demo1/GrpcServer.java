package cn.yase.share4.demo1;

import cn.yase.share4.proto.GreeterGrpc;
import cn.yase.share4.proto.HelloReply;
import cn.yase.share4.proto.HelloRequest;
import com.google.common.base.Charsets;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.options.PutOption;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * @author yase
 * @create 2019-05-24
 */
public class GrpcServer {

    private Server server;

    private final int port;

    public GrpcServer(int port) {
        this.port = port;
    }

    private void start() throws IOException, ExecutionException, InterruptedException {
        this.server = ServerBuilder.forPort(port).addService(new GrpcServerImpl()).build().start();

        //注册并保持心跳
        register();

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void register() throws ExecutionException, InterruptedException {
        Client client = Client.builder().endpoints("http://127.0.0.1:2379").build();

        long leaseId = client.getLeaseClient().grant(5).get().getID();

        //本地IP地址
        String address = "10.20.0.108:"+port;
        ByteSequence key = ByteSequence.from("/grpc/java/" + address, Charsets.UTF_8);
        ByteSequence value = ByteSequence.from(address, Charsets.UTF_8);

        PutOption putOption = PutOption.newBuilder().withLeaseId(leaseId).build();
        client.getKVClient().put(key,value,putOption).get();

        client.getLeaseClient().keepAlive(leaseId, new StreamObserver<LeaseKeepAliveResponse>() {
            @Override
            public void onNext(LeaseKeepAliveResponse leaseKeepAliveResponse) {
                System.out.println(leaseId+"");
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println(throwable);
            }

            @Override
            public void onCompleted() {
                System.out.println("onCompleted!");
            }
        });
    }

    private void shutdown(){
        if (this.server != null){
            this.server.shutdown();
        }
    }

    private void await() throws InterruptedException {
        if (this.server != null){
            this.server.awaitTermination();
        }
    }

    static class GrpcServerImpl extends GreeterGrpc.GreeterImplBase {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            System.out.println("接收到客户端信息:"+request.getName());
            responseObserver.onNext(HelloReply.newBuilder().setMessage("你好"+request.getName()).build());
            responseObserver.onCompleted();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        GrpcServer grpcServer = new GrpcServer(8894);

        grpcServer.start();
        grpcServer.await();
    }


}
