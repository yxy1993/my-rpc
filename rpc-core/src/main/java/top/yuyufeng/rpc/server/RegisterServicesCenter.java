package top.yuyufeng.rpc.server;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 服务注册中心
 * @author yuyufeng
 */
public class RegisterServicesCenter {
    private static String zookeeperHost;
    private static ZooKeeper zookeeper;
    private static int TIME_OUT = 10000;
    private static String localIp;

    public static void init(String zookeeperHost, int localPort) {
        RegisterServicesCenter.zookeeperHost = zookeeperHost;
        try {
            zookeeper = new ZooKeeper(zookeeperHost, TIME_OUT, null);

            //提供层的ip,这里存放本机的ip
            localIp = InetAddress.getLocalHost().getHostAddress() + ":" + localPort;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 暴露接口的实现类存放容器
     */
    private static ConcurrentHashMap<String, Class> registerServices = new ConcurrentHashMap<>();

    /**
     * 增加服务
     * @param className
     * @param clazz
     * @throws InterruptedException
     */
    public static void addServices(String className, Class clazz) throws InterruptedException {
        registerServices.put(className, clazz);
        //存入zookeeper节点
        //后期优化增强:一个服务可配置多个ip,这样就可以有多个提供层提供服务,在客户端使用负载均衡策略即可
        try {
            if (zookeeper.exists("/myrpc", false) == null) {
                zookeeper.create("/myrpc", "true".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            if (zookeeper.exists("/myrpc/" + className, false) != null) {
                zookeeper.delete("/myrpc/" + className, -1);
            }
            //把当前服务的ip地址存如zookeeper中,供消费者发现
            zookeeper.create("/myrpc/" + className, localIp.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (KeeperException e) {
            e.printStackTrace();
        } finally {
//            zookeeper.close();
        }
    }

    /**
     * 根据类名获取服务
     * @param className
     * @return
     */
    public static Class getService(String className) {
        return registerServices.get(className);
    }
}
