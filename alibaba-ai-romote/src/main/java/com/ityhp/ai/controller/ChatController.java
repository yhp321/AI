package com.ityhp.ai.controller;
import com.alibaba.dashscope.aigc.generation.*;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class ChatController {

    private static StringBuilder fullContent = new StringBuilder();
    private static boolean isFirstPrint = true;

    private static void handleGenerationResult(GenerationResult message) {
        String reasoning = message.getOutput().getChoices().get(0).getMessage().getReasoningContent();
        String content = message.getOutput().getChoices().get(0).getMessage().getContent();
        SearchInfo searchInfo = message.getOutput().getSearchInfo();
        if (!reasoning.isEmpty()) {
            if (isFirstPrint) {
                fullContent.append("====================搜索信息====================\n");
                isFirstPrint = false;
                fullContent.append(searchInfo.getSearchResults());
                fullContent.append("====================思考过程====================\n");
            }
            fullContent.append(reasoning);
        }

        if (!content.isEmpty()) {
            if (!isFirstPrint) {
                fullContent.append("\n====================完整回复====================\n");
                isFirstPrint = true;
            }
            fullContent.append(content);
        }
        if (message.getOutput().getChoices().get(0).getFinishReason().equals("stop") || message.getOutput().getChoices().get(0).getFinishReason().equals("length")){
            fullContent.append("\n====================Token 消耗====================\n");
            fullContent.append(message.getUsage());
        }
    }
    @RequestMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public String streamCallWithMessage(@RequestParam("prompt") String prompt,
            @RequestParam("chatId") String chatId,
            @RequestParam(value = "files", required = false) List<MultipartFile> files)
            throws NoApiKeyException, ApiException, InputRequiredException {
        Generation gen = new Generation();
        Message userMsg = Message.builder().role(Role.USER.getValue()).content(prompt).build();
        GenerationParam param = buildGenerationParam(userMsg);
        System.out.println("开始思考");
        Flowable<GenerationResult> result = gen.streamCall(param);
        result.blockingForEach(message -> handleGenerationResult(message));
        System.out.println("思考结束");
        return fullContent.toString();
    }
    private static GenerationParam buildGenerationParam(Message userMsg) {
         // 定义搜索策略
        SearchOptions searchOptions = SearchOptions.builder()
                // 使返回结果中包含搜索信息的来源
                .enableSource(true)
                // 强制开启互联网搜索
                .forcedSearch(true)
                // 开启角标标注
                .enableCitation(true)
                // 设置角标标注样式为[ref_i]
                .citationFormat("[ref_<number>]")
                // 联网搜索10条信息
                .searchStrategy("pro")
                .build();
        return GenerationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey("sk-key")
                .model("qwq-plus")   // 此处以qwen-plus为例，您可按需更换模型名称。模型列表：https://help.aliyun.com/zh/model-studio/getting-started/models
                .messages(Arrays.asList(userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .enableSearch(true)
                .searchOptions(searchOptions)
                .incrementalOutput(true)
                .build();
    }

}
