package com.teambind.image_server.entity.key;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * ImageSequence 복합 키 클래스
 *
 * @see com.teambind.image_server.entity.ImageSequence
 * @deprecated 더 이상 사용하지 않습니다. ImageSequence 엔티티가 단순 Long ID를 사용하도록 변경되었습니다.
 * 하위 호환성을 위해 클래스는 유지하지만 새 코드에서는 사용하지 마세요.
 */
@Deprecated(since = "2.0", forRemoval = true)
@Getter
@Setter
@EqualsAndHashCode
public class ImageSequenceId implements Serializable {
	private String imageId;
	private Long id;
}
