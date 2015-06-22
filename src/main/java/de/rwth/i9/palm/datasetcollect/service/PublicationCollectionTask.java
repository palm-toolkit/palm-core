package de.rwth.i9.palm.datasetcollect.service;

import de.rwth.i9.palm.concurrent.ObjectPool;

public class PublicationCollectionTask implements Runnable
{

	private ObjectPool<PublicationCollection> pool;

	private PublicationCollection publicationCollection;

	private int threadNo;

	public PublicationCollectionTask( ObjectPool<PublicationCollection> pool, int threadNo )
	{
		this.pool = pool;
		this.threadNo = threadNo;
	}

	public PublicationCollectionTask( ObjectPool<PublicationCollection> pool, int threadNo, GoogleScholarPublicationCollection googleSchol )
	{
		this.pool = pool;
		this.threadNo = threadNo;
		this.publicationCollection = googleSchol;
	}


	@Override
	public void run()
	{
		// get an object from the pool
		this.publicationCollection = pool.borrowObject();

		System.out.println( "Thread " + threadNo + ": Object with process no. " + publicationCollection.getProcessNo() + " was borrowed" );

		// do something
		// ...
		System.out.println( this.publicationCollection.getLocation() );

		// for-loop is just for simulation
		for ( int i = 0; i < 100000; i++ )
		{
		}

		// return publicationCollection instance back to the pool
		pool.returnObject( publicationCollection );

		System.out.println( "Thread " + threadNo + ": Object with process no. " + publicationCollection.getProcessNo() + " was returned" );

	}

}
