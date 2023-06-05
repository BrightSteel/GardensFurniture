package com.gardensmc.gardensfurniture.custom;

public class CustomCaveVineBlock extends CustomBlock {

    private int age;

    public CustomCaveVineBlock(int age, boolean seated, boolean cancel_drop) {
        super(seated, cancel_drop);
        this.age = age;
    }

    public int getAge() {
        return age;
    }

}
