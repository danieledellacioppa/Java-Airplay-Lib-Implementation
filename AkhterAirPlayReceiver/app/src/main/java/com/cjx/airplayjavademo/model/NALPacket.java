package com.cjx.airplayjavademo.model;

public class NALPacket {
    public byte[] nalData = null;
    public int nalType = 0;
    public long pts = 0;
    public long sequenceNumber = 0;
    public boolean codecConfig = false;
}
