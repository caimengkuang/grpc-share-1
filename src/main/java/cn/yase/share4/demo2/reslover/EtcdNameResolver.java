package cn.yase.share4.demo2.reslover;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * @author yase
 * @create 2019-05-24
 */
public class EtcdNameResolver extends NameResolver {

    private final ByteSequence prefix;

    private final Set<String> urls;

    private Client client;

    private Listener listener;

    public EtcdNameResolver(String path){

        this.prefix = ByteSequence.from(path, Charsets.UTF_8);

        this.urls = new HashSet<>();

        this.client = Client.builder().endpoints("http://127.0.0.1:2379").build();
    }

    @Override
    public String getServiceAuthority() {
        return prefix.toString(Charsets.UTF_8);
    }

    @Override
    public void start(Listener listener) {
        synchronized (this){
            Preconditions.checkState(this.listener==null,"listen is already start");
            this.listener = listener;
            initAndWatch();
        }
    }

    private void initAndWatch(){
        //获取当前可用服务器列表
        getUrls();

        //刷新服务器列表
        update();

        //监听服务器
        watch();
    }

    private void watch(){

        WatchOption watchOption = WatchOption.newBuilder().withPrefix(prefix).build();

        client.getWatchClient().watch(prefix, watchOption, new Watch.Listener() {
            @Override
            public void onNext(WatchResponse watchResponse) {
                List<WatchEvent> events = watchResponse.getEvents();

                for (WatchEvent watchEvent : events){
                    WatchEvent.EventType eventType = watchEvent.getEventType();

                    switch (eventType){
                        case PUT:{
                            String address = watchEvent.getKeyValue().getValue().toString(Charsets.UTF_8);
                            urls.add(address);
                            update();
                            break;
                        }
                        case DELETE:{
                            String address = watchEvent.getKeyValue().getValue().toString(Charsets.UTF_8);
                            urls.remove(address);
                            update();
                            break;
                        }
                        case UNRECOGNIZED:{
                            break;
                        }
                    }

                }

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });

    }

    private void update(){
        List<EquivalentAddressGroup> equivalentAddressGroups = new ArrayList<>(urls.size());

        for (String url : urls){
            String[] split = url.split(":");
            InetSocketAddress socketAddress = new InetSocketAddress(split[0], Integer.parseInt(split[1]));

            EquivalentAddressGroup equivalentAddressGroup = new EquivalentAddressGroup(socketAddress);
            equivalentAddressGroups.add(equivalentAddressGroup);
        }

        if (!equivalentAddressGroups.isEmpty()){
            this.listener.onAddresses(equivalentAddressGroups, Attributes.EMPTY);
        }

    }

    private void getUrls(){
        //根据前缀从etcd中获取数据
        GetOption getOption = GetOption.newBuilder().withPrefix(prefix).build();
        try {
            GetResponse getResponse = client.getKVClient().get(prefix, getOption).get();

            List<KeyValue> kvs = getResponse.getKvs();

            for (KeyValue keyValue : kvs){
                String address = keyValue.getValue().toString(Charsets.UTF_8);
                urls.add(address);
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void shutdown() {
        if (this.client != null){
            this.client.close();
        }
    }
}
