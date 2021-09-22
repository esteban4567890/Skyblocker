package me.xmrvizzy.skyblocker.skyblock.item;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class PriceInfoTooltip {
    private JsonObject auctionPricesJson = null;
    private JsonObject bazaarPricesJson = null;
    public static JsonObject prices;

    public static void onInjectTooltip(ItemStack stack, TooltipContext context, List<Text> list) {
        String name = getInternalNameForItem(stack);

        try {
            if(!list.toString().contains("Avg. BIN Price") && prices != null && prices.has(name) ){
                JsonElement getPrice = prices.get(name);
                String price = round(getPrice.getAsDouble(), 2);

                list.add(new LiteralText("Avg. BIN Price: ").formatted(Formatting.GOLD).append(new LiteralText(price + " Coins").formatted(Formatting.DARK_AQUA)));
            }
        }catch(Exception e) {
            MinecraftClient.getInstance().player.sendMessage(new LiteralText(e.toString()), false);
        }

	}
    public static String round(double value, int places) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return df.format(bd);
    }
    public static String getInternalNameForItem(ItemStack stack) {
        if(stack == null) return null;
        NbtCompound tag = stack.getNbt();
        return getInternalnameFromNBT(tag);
    }

    public static String getInternalnameFromNBT(NbtCompound tag) {
        String internalname = null;
        if(tag != null && tag.contains("ExtraAttributes", 10)) {
            NbtCompound  ea = tag.getCompound("ExtraAttributes");

            if(ea.contains("id", 8)) {
                internalname = ea.getString("id").replaceAll(":", "-");
            } else {
                return null;
            }


            if("ENCHANTED_BOOK".equals(internalname)) {
                NbtCompound enchants = ea.getCompound("enchantments");

                for(String enchname : enchants.getKeys()) {
                    internalname = enchname.toUpperCase() + ";" + enchants.getInt(enchname);
                    break;
                }
            }
        }

        return internalname;
    }

    public static void init() {
        MinecraftClient.getInstance().execute(PriceInfoTooltip::downloadPrices);
    }

    private static void downloadPrices() {
        JsonObject result = null;
        try {
            URL apiAddr = new URL("https://moulberry.codes/auction_averages_lbin/3day.json.gz");
            try (InputStream src = apiAddr.openStream()) {
                try (GZIPInputStream gzipOutput = new GZIPInputStream(src)) {
                    try (InputStreamReader reader = new InputStreamReader(gzipOutput)) {
                        result = new Gson().fromJson(reader, JsonObject.class);
                    }
                }
            }
        }
        catch(IOException e) {
            LogManager.getLogger(PriceInfoTooltip.class.getName()).warn("[Skyblocker] Failed to download item prices!", e);
        }
        prices = result;
    }
}