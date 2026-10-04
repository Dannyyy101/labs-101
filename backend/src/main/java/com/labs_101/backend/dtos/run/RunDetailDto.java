package com.labs_101.backend.dtos.run;

import java.util.List;

public record RunDetailDto(RunSummaryDto summary, List<RunPointDto> points, List<RunSplitDto> splits) {
}
