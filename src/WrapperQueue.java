import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class WrapperQueue {
	private final AtomicReference<MyArrayBlockingQueue> producerRef = new AtomicReference<>() ;
	private final AtomicReference<MyArrayBlockingQueue> consumerRef = new AtomicReference<>() ;
	private final ObjectStore fileQueue;

	private final int capacity;

	private final Lock producerReplaceLock = new ReentrantLock();
	private final Lock consumerReplaceLock = new ReentrantLock();

	private final ExecutorService pool = Executors.newCachedThreadPool();


	public WrapperQueue(int capacity) {

		producerRef.set(new MyArrayBlockingQueue(capacity));
		consumerRef.set(new MyArrayBlockingQueue(capacity));
		fileQueue = new ObjectStore();
		this.capacity = capacity;

		Thread moveFromFileDaemon = new Thread(() -> {
			while (true) moveFromFile();
		});
		moveFromFileDaemon.setDaemon(true);
		moveFromFileDaemon.start();
	}

	private void moveToFile(MyArrayBlockingQueue queue) {
		try {
			fileQueue.writeQueue(queue);
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}

	private void moveFromFile() {
		MyArrayBlockingQueue nextConsumer = fileQueue.readQueue();
		try {
			// Will wait until consumer gets empty
	    	while(consumerRef.get().size()>0)
	    		;
	    	if(nextConsumer!=null)
	    	  consumerRef.set(nextConsumer);
	    	
		} catch (Exception e) {
			throw new RuntimeException(e) ;
		}
	}

	public void put(String e) {
		if(producerRef.get().queueFull()) {
			producerReplaceLock.lock() ;
			try {
				if(producerRef.get().queueFull()) {
					MyArrayBlockingQueue producerCopy = producerRef.get();
					producerRef.set(new MyArrayBlockingQueue(capacity));
					pool.execute(() -> moveToFile(producerCopy));
				}
			} finally {
				producerReplaceLock.unlock() ;
			}
		}

		producerRef.get().put(e);
	}

	public String take() {
		consumerReplaceLock.lock() ;
		try {
		while (consumerRef.get().queueEmpty()) {
			if(fileQueue.size() == 0) {
				return producerRef.get().take() ;
			}
			} 
		}
		catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				consumerReplaceLock.unlock();
			}
		

		return consumerRef.get().take();
	}
}