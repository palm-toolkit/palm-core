package de.rwth.i9.palm.config;

import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import de.rwth.i9.palm.analytics.api.PalmAnalyticsImpl;
import de.rwth.i9.palm.feature.researcher.ResearcherFeature;
import de.rwth.i9.palm.feature.researcher.ResearcherFeatureImpl;

//import de.rwth.i9.palm.analytics.api.PalmAnalyticsImpl;

@Configuration
@ComponentScan( { "de.rwth.i9.palm" } )
@PropertySource( "classpath:application.properties" )
@EnableAsync
@Lazy( true )
public class WebAppConfigTest extends WebMvcConfigurerAdapter implements AsyncConfigurer
{

	@Autowired
	private Environment env;

	/* resource */

	// Maps resources path to webapp/resources
	public void addResourceHandlers( ResourceHandlerRegistry registry )
	{
		registry.addResourceHandler( "/resources/**" ).addResourceLocations( "/resources/" );
	}


	/* fileupload */

	// @Bean
	// public CommonsMultipartResolver commonsMultipartResolver()
	// {
	// CommonsMultipartResolver commonsMultipartResolver = new
	// CommonsMultipartResolver();
	// commonsMultipartResolver.setMaxUploadSize( 10000000 );
	// return commonsMultipartResolver;
	// }

	@Bean( name = "multipartResolver" )
	public CommonsMultipartResolver createMultipartResolver()
	{
		CommonsMultipartResolver resolver = new CommonsMultipartResolver();
		resolver.setDefaultEncoding( "utf-8" );
		return resolver;
	}


	/* palm analytics */
	@Bean
	@Scope( "singleton" )
	public PalmAnalyticsImpl configAnalyticsImpl()
	{
		return new PalmAnalyticsImpl();
	}

	/* palm researcher feature */
	@Bean
	@Scope( "singleton" )
	public ResearcherFeature researcherFeature()
	{
		return new ResearcherFeatureImpl();
	}

	/* Scheduling and ThreadPool */

	@Override
	public Executor getAsyncExecutor()
	{
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setMaxPoolSize( 100 );
		taskExecutor.setCorePoolSize( 20 );
		taskExecutor.setQueueCapacity( 1000 );
		taskExecutor.setThreadNamePrefix( "PALMExecutor-" );
		taskExecutor.initialize();
		return taskExecutor;
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler()
	{
		return new SimpleAsyncUncaughtExceptionHandler();
	}
}
