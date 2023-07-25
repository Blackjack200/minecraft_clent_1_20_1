package com.mojang.realmsclient.client;

import com.google.common.collect.Lists;
import com.mojang.realmsclient.dto.RegionPingResult;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Comparator;
import java.util.List;
import net.minecraft.Util;
import org.apache.commons.io.IOUtils;

public class Ping {
   public static List<RegionPingResult> ping(Ping.Region... aping_region) {
      for(Ping.Region ping_region : aping_region) {
         ping(ping_region.endpoint);
      }

      List<RegionPingResult> list = Lists.newArrayList();

      for(Ping.Region ping_region1 : aping_region) {
         list.add(new RegionPingResult(ping_region1.name, ping(ping_region1.endpoint)));
      }

      list.sort(Comparator.comparingInt(RegionPingResult::ping));
      return list;
   }

   private static int ping(String s) {
      int i = 700;
      long j = 0L;
      Socket socket = null;

      for(int k = 0; k < 5; ++k) {
         try {
            SocketAddress socketaddress = new InetSocketAddress(s, 80);
            socket = new Socket();
            long l = now();
            socket.connect(socketaddress, 700);
            j += now() - l;
         } catch (Exception var12) {
            j += 700L;
         } finally {
            IOUtils.closeQuietly(socket);
         }
      }

      return (int)((double)j / 5.0D);
   }

   private static long now() {
      return Util.getMillis();
   }

   public static List<RegionPingResult> pingAllRegions() {
      return ping(Ping.Region.values());
   }

   static enum Region {
      US_EAST_1("us-east-1", "ec2.us-east-1.amazonaws.com"),
      US_WEST_2("us-west-2", "ec2.us-west-2.amazonaws.com"),
      US_WEST_1("us-west-1", "ec2.us-west-1.amazonaws.com"),
      EU_WEST_1("eu-west-1", "ec2.eu-west-1.amazonaws.com"),
      AP_SOUTHEAST_1("ap-southeast-1", "ec2.ap-southeast-1.amazonaws.com"),
      AP_SOUTHEAST_2("ap-southeast-2", "ec2.ap-southeast-2.amazonaws.com"),
      AP_NORTHEAST_1("ap-northeast-1", "ec2.ap-northeast-1.amazonaws.com"),
      SA_EAST_1("sa-east-1", "ec2.sa-east-1.amazonaws.com");

      final String name;
      final String endpoint;

      private Region(String s, String s1) {
         this.name = s;
         this.endpoint = s1;
      }
   }
}