package Yin.rpc.cousumer.zk;

import java.util.HashSet;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.WatchedEvent;

import Yin.rpc.cousumer.core.ChannelManager;
import Yin.rpc.cousumer.core.NettyClient;
import io.netty.channel.ChannelFuture;

public class ServerWatcher implements CuratorWatcher {
	//当服务端的一些事件触发了这个Watcher，那么就会向指定客户端发送一个事件通知（WatcherEvent）
	public void process(WatchedEvent event) throws Exception {//WatchedEvent event
		System.out.println("process----------------------");
		CuratorFramework client = ZooKeeperFactory.getClient();
		//获取变更数据后的服务端注册列表数据
		String path = event.getPath();
		//watcher是一次性的，当事件被触发之后，所对应的watcher会被立马删除，如果要反复使用，就需要反复的使用usingWatcher提前注册
		client.getChildren().usingWatcher(this).forPath(path);
		//返回新的注册类别信息List<String>
		List<String> newServerPaths = client.getChildren().forPath(path);
		System.out.println(newServerPaths);
		//清除旧的注册类别信息
		ChannelManager.realServerPath.clear();
		for(String p :newServerPaths){
			String[] str = p.split("#");
			//将新的类别信息赋值给通道管理器当中 注：一开始通道为0
			ChannelManager.realServerPath.add(str[0]+"#"+str[1]);//去重
		}

		//清除集合里旧的通道
		ChannelManager.clearChannel();
		for(String realServer:ChannelManager.realServerPath){
			String[] str = realServer.split("#");
			//获取新的注册类信息连接通道
			ChannelFuture channnelFuture = NettyClient.b.connect(str[0], Integer.valueOf(str[1]));
			ChannelManager.addChnannel(channnelFuture);		
		}
	}
}
