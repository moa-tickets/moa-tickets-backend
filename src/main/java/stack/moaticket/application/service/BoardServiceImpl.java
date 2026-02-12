package stack.moaticket.application.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stack.moaticket.application.dto.BoardDto;
import stack.moaticket.domain.board.entity.Board;
import stack.moaticket.domain.board.repository.BoardRepository;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class BoardServiceImpl implements BoardService {
    private final Validator validator;
    private final BoardRepository boardRepository;
    private final MemberService memberService;

    @Override
    public void create(Long memberId, BoardDto.Request boardRequest) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        Board board = requestToEntity(member, boardRequest);

        boardRepository.save(board);
    }

    @Override
    public BoardDto.BoardResponse read(Long id) {
//        if(id <0)  throw new MoaException(MoaExceptionType.MEMBER_NOT_FOUND("Id가 음수가 될수 없습니다"));

        Board boardEntity = boardRepository.findById(id).orElseThrow(() ->
                new MoaException(MoaExceptionType.ENTITY_NOT_FOUND));
        return entityToResponse(boardEntity);
    }

    @Override
    public List<BoardDto.BoardResponse> reads() {
        List<Board> boardList = boardRepository.findAll();
        List<BoardDto.BoardResponse> res = new ArrayList<>();

        for (Board b : boardList) {
            res.add(this.entityToResponse(b));
        }
        return res;
    }

    @Override
    public void fix(Long memberId, BoardDto.BoardFixRequest boardFixRequest, Long boardId) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        Board boardEntity = boardRepository.findById(boardId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.ENTITY_NOT_FOUND));

        if (!boardEntity.getMember().getId().equals(memberId)) {
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }
        boardEntity.fix(boardFixRequest);
        //가독성을 위해 save을 한 번더 작성
        boardRepository.save(boardEntity);
    }

    @Override
    public void delete(Long memberId, Long boardId) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        Board boardEntity = boardRepository.findById(boardId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.ENTITY_NOT_FOUND));

        if (!boardEntity.getMember().getId().equals(memberId)) {
            throw new MoaException(MoaExceptionType.FORBIDDEN,
                    "memberId가 같지 않습니다");
        }
        boardRepository.deleteById(boardId);
    }
}