package com.example.githubstats.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // Ensure Async is enabled here or on the main Application class
public class AsyncConfig {

    // Define a specific executor bean for Bitbucket processing tasks
    // This bean name ("bitbucketTaskExecutor") will be used in @Async annotation
    @Bean("bitbucketTaskExecutor")
    public Executor bitbucketTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Adjust core/max pool size based on your resources and expected number of projects
        executor.setCorePoolSize(5);  // Number of threads to keep alive
        executor.setMaxPoolSize(10); // Maximum number of threads allowed
        executor.setQueueCapacity(25); // Max number of tasks waiting in queue
        executor.setThreadNamePrefix("BitbucketWorker-");
        executor.initialize();
        return executor;
    }

    // You could define other executors for different async tasks if needed
}