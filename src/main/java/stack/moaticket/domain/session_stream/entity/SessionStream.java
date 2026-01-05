package stack.moaticket.domain.session_stream.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.session_stream.type.SessionStreamState;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(name = "session_stream")
public class SessionStream extends Base {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_stream_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "session_id")
    private Session session;

    @Column(name = "stream_key", nullable = false, unique = true)
    private String streamKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_stream_state", nullable = false)
    private SessionStreamState state;

    @Column(name = "playback_id", nullable = false, unique = true)
    private String playbackId;
}
