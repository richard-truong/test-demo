package com.example.testdemo.adapter.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Publishes the {@link UserMcpTools} methods as MCP tools.
 *
 * <p>The MCP server auto-configuration collects every {@link ToolCallbackProvider}
 * bean in the context and exposes the tools it returns.
 */
@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider userToolCallbackProvider(UserMcpTools userMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(userMcpTools)
                .build();
    }
}
