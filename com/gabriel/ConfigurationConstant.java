package com.gabriel;

public class ConfigurationConstant{
	/**    最大线程池连接数    */

	public static final String MAX_CONNECTION_NUM  = "max.connection.num";

	/**    线程池初始数目    */

	public static final String INITIAL_CONNECTION_NUM  = "initial.connection.num";

	/**    空闲连接shrink时候最少保留数目    */

	public static final String IDLE_MIN_POOL_NUM  = "idle.min.pool.num";

	/**    新建连接失败时重试多少次,进行rest    */

	public static final String MAX_CREATE_ERROR_COUNT  = "max.create.error.count";

	/**    达到最大重试次数rest time millions    */

	public static final String CREATE_ERROR_SLEEP_TIME  = "create.error.sleep.time";

	/**    检查未归还和池内空闲连接的间隔millions    */

	public static final String EVICT_CHECK_INTERVAL  = "evict.check.interval";

	/**    空闲多长时间就被清除 millions    */

	public static final String EVICT_IDLE_TIME  = "evict.idle.time";

	/**    是否移除未归还的    */

	public static final String ABANDON_NEED_REMOVE  = "abandon.need.remove";

	/**    多久未归还需要移除    */

	public static final String ABANDON_NEED_LAST_TIME  = "abandon.need.last.time";

	/**    从池中拿到连接是否要检查    */

	public static final String TEST_ON_BORROW  = "test.on.borrow";

	/**    空闲时间达到的时候检查    */

	public static final String TEST_ON_IDLE  = "test.on.idle";

	/**    物理连接的class    */

	public static final String RAW_CONNECTION_CLASS  = "raw.connection.class";


}