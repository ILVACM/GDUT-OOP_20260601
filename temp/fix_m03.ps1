$file = "d:\GDUT-OOP_20260601\wiki\modules\M03-Exam-Assembly.md"
$content = Get-Content $file -Encoding UTF8 -Raw

# Replace 1: §3.2 heading and JSON example
$old1 = "### 3.2 自动组卷（AUTO）`n`n教师配置规则，系统按规则从题库抽题。`n`n**输入**（自动组卷规则）："
$new1 = "### 3.2 自动组卷（AUTO）—— 最简实现`n`n教师仅指定题目数量，系统从题库中**完全随机**抽取指定数量的题目。**不支持**按题型、难度、性质等任何维度的筛选规则。`n`n**输入**（自动组卷请求）："
if ($content.Contains($old1)) { $content = $content.Replace($old1, $new1); Write-Host "REPLACED §3.2 heading" } else { Write-Host "§3.2 heading NOT FOUND" }

# Replace 2: Old JSON example block
$old2 = "```json`n{`n  ""name"": ""自动组卷 - 词汇专项"","
$new2 = "```json`n{`n  ""name"": ""随机组卷 - 期末复习"","
if ($content.Contains($old2)) { $content = $content.Replace($old2, $new2); Write-Host "REPLACED JSON example" } else { Write-Host "JSON example NOT FOUND" }

# Replace 3: Old constraints structure
$old3 = "  ""autoRule"": {`n    ""totalScore"": 100,`n    ""constraints"": ["
$new3 = "  ""autoRule"": {`n    ""totalQuestions"": 20,`n    ""totalScore"": 100`n  }`n}`n````n`n**后端处理**（核心算法）：`n`n```java`npublic List<ExamQuestionItem> autoAssemble(AutoRule rule) {`n    // 1. 从题库拉取所有已发布题目`n    List<Question> candidates = questionRepository.findByStatus(QuestionStatus.PUBLISHED);`n`n    // 2. 校验候选数是否足够`n    if (candidates.size() < rule.getTotalQuestions()) {`n        throw new BusinessException(4302, ""题库中可用题目不足：需要 "" + rule.getTotalQuestions() + "" 道，实际 "" + candidates.size() + "" 道"");`n    }`n`n    // 3. 完全随机抽取 N 道`n    Collections.shuffle(candidates);`n    List<Question> picked = candidates.subList(0, rule.getTotalQuestions());`n`n    // 4. 等分计算每题分值`n    int scoreEach = rule.getTotalScore() / rule.getTotalQuestions();`n    int remainder = rule.getTotalScore() % rule.getTotalQuestions();`n`n    List<ExamQuestionItem> result = new ArrayList<>();`n    for (int i = 0; i < picked.size(); i++) {`n        // 余数分摊到前 N 道题（每道多 1 分），保证总分精确`n        int score = scoreEach + (i < remainder ? 1 : 0);`n        result.add(new ExamQuestionItem(picked.get(i).getId(), score));`n    }`n    return result;`n}`n```"
$marker = "      { ""questionType"": ""FILL_BLANK"", ""count"": 5, ""scoreEach"": 2, ""propertyIds"": [1, 2] }`n    ]`n  }`n}"
if ($content.Contains($marker)) { $content = $content.Replace($marker, $new3); Write-Host "REPLACED constraints structure" } else { Write-Host "constraints structure NOT FOUND" }

# Replace 4: Old algorithm code
$old4 = "public List<ExamQuestionItem> autoAssemble(AutoRule rule) {`n    List<ExamQuestionItem> result = new ArrayList<>();`n    for (Constraint c : rule.getConstraints()) {`n        // 1. 按类型 + 难度 + 性质 + status=1 检索候选题`n        List<Question> candidates = questionRepository.findByAutoCriteria(`n            c.getQuestionType(),`n            c.getDifficulty(),`n            c.getPropertyIds()`n        );"
if ($content.Contains($old4)) { $content = $content.Replace($old4, "// ALREADY REPLACED BY §3.2 algorithm"); Write-Host "Old algorithm REMOVED" } else { Write-Host "Old algorithm NOT FOUND (might be already gone)" }

# Replace 5: AutoConstraint DTO
$old5 = "public record AutoRule(`n    @NotNull @Min(1) Integer totalScore,`n    @NotEmpty @Valid List<AutoConstraint> constraints`n) {}`n`npublic record AutoConstraint(`n    @NotNull QuestionType questionType,`n    @NotNull @Min(1) Integer count,`n    @NotNull @Min(1) Integer scoreEach,`n    List<Integer> difficulty,         // 难度区间，如 [1,2,3]`n    List<Long> propertyIds`n) {}"
$new5 = "public record AutoRule(`n    @NotNull @Min(1) Integer totalQuestions,  // 抽取的题目数量`n    @NotNull @Min(1) Integer totalScore        // 试卷总分`n) {}"
if ($content.Contains($old5)) { $content = $content.Replace($old5, $new5); Write-Host "REPLACED AutoRule+AutoConstraint DTO" } else { Write-Host "AutoRule DTO NOT FOUND" }

# Replace 6: Old "设计权衡" 注释 (in 8.1 area)
$old6 = "**校验时间、时长、总分              **`n  |  对每个 constraint:                **`n  |  SELECT question WHERE ...      **"
$new6 = "**校验时间、时长、总分              **`n  |  SELECT * FROM question           **`n  |  WHERE status = 1                 **"
if ($content.Contains($old6)) { $content = $content.Replace($old6, $new6); Write-Host "REPLACED 8.1 flow partial" } else { Write-Host "8.1 flow NOT FOUND" }

# Replace 7: Remove "对每个 constraint" line in 8.1
$old7 = "                                  |  对每个 constraint:                |`n                                  |    SELECT question WHERE ...      |"
$new7 = "                                  |  SELECT * FROM question           |`n                                  |  WHERE status = 1                 |"
if ($content.Contains($old7)) { $content = $content.Replace($old7, $new7); Write-Host "REPLACED 8.1 flow constraint" } else { Write-Host "8.1 constraint line NOT FOUND" }

Set-Content $file -Value $content -Encoding UTF8 -NoNewline
Write-Host "DONE. File saved."
