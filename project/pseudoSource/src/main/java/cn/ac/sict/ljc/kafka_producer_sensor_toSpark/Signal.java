package cn.ac.sict.ljc.kafka_producer_sensor_toSpark;

/**
 * 所有传感器信号的父类, 以后的新的传感器信号必须继承此类
 * 
 */
public abstract class Signal {
	public long id;
	public long time;
}
