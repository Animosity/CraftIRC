package com.ensifera.animosity.craftirc;

import java.util.TimerTask;

public class RetryTask extends TimerTask {
    
    private Minebot bot;
    private String channel;
    private CraftIRC plugin;
    RetryTask(CraftIRC plugin, Minebot bot, String channel) {
        this.bot = bot;
        this.channel = channel;
        this.plugin = plugin;
    }

    public void run() {
        if (channel == null) {
            if (!bot.isConnected()) {
                bot.connectToIrc();
                plugin.scheduleForRetry(bot, null);
            }
        } else {
            if (!bot.isIn(channel)) {
                bot.joinIrcChannel(channel);
                plugin.scheduleForRetry(bot, channel);
            }
        }
    }

}
