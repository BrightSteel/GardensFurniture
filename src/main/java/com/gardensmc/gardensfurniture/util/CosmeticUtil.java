package com.gardensmc.gardensfurniture.util;

public class CosmeticUtil {

    // takes cosmetic permission, ie witch_hat and makes it into Witch Hat Cosmetic
    public static String formatName(String cosmetic) {
        String[] parts = cosmetic.split("_");
        StringBuilder stringBuilder = new StringBuilder();
        for (String part : parts) {
            stringBuilder.append(part.toUpperCase().charAt(0)).append(part.substring(1)).append(" ");
        }
        stringBuilder.append("Cosmetic");
        return stringBuilder.toString();
    }
}
