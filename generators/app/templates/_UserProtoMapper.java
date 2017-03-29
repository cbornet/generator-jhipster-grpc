package <%=packageName%>.grpc;

import <%=packageName%>.domain.User;
import <%=packageName%>.service.dto.UserDTO;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

import java.util.HashSet;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class UserProtoMapper extends ProtobufUtil {

    public UserDTO userProtoToUserDTO(UserProto userProto) {
        if ( userProto == null ) {
            return null;
        }
        return new UserDTO(
            userProto.getIdOneofCase() == UserProto.IdOneofCase.ID ? userProto.getId() : null,
            userProto.getLogin().isEmpty() ? null : userProto.getLogin(),
            userProto.getFirstName().isEmpty() ? null : userProto.getFirstName(),
            userProto.getLastName().isEmpty() ? null : userProto.getLastName(),
            userProto.getEmail().isEmpty() ? null : userProto.getEmail(),
            userProto.getActivated(),
            <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
            userProto.getImageUrl().isEmpty() ? null : userProto.getImageUrl(),
            <%_ } _%>
            userProto.getLangKey().isEmpty() ? null : userProto.getLangKey(),
            <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
            userProto.getCreatedBy().isEmpty() ? null : userProto.getCreatedBy(),
            timestampToZonedDateTime(userProto.getCreatedDate()),
            userProto.getLastModifiedBy().isEmpty() ? null: userProto.getLastModifiedBy(),
            timestampToZonedDateTime(userProto.getLastModifiedDate()),
            <%_ } _%>
            new HashSet<>(userProto.getAuthoritiesList())
        );
    }

    public UserProto userDTOToUserProto(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }
        UserProto.Builder builder = userDTOToUserProtoBuilder(userDTO);
        // Authorities were not mapped because the method name doesn't match javabean setter names.
        if (userDTO.getAuthorities() != null) {
            builder.addAllAuthorities(userDTO.getAuthorities());
        }
        return builder.build();
    }

    UserProto.Builder createUserProto () {
        return UserProto.newBuilder();
    }

    protected abstract UserProto.Builder userDTOToUserProtoBuilder (UserDTO userDTO);

    public UserProto userToUserProto(User user) {
        if (user == null) {
            return null;
        }
        return userToUserProtoBuilder(user).build();
    }

    @Mapping(target = "password", ignore = true)
    protected abstract UserProto.Builder userToUserProtoBuilder (User user);

}
